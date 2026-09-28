# Campus Swap Frontend

React + TypeScript + Vite frontend for the Campus Swap marketplace.

## Features

- ✅ **Authentication** - Register, login, JWT token handling
- ✅ **Browse Listings** - Search, filter, pagination
- ✅ **Listing Management** - Create, edit, delete, mark as sold
- ✅ **Wishlist** - Save favorite items
- ✅ **Reports** - Report suspicious listings
- ✅ **Responsive Design** - Mobile-friendly UI
- ✅ **Real-time Feedback** - Toast notifications
- ✅ **Form Validation** - Client-side validation
- ✅ **Protected Routes** - Role-based access control
- ✅ **Error Handling** - User-friendly error messages

## Prerequisites

- Node.js 18+ and npm
- Backend API running on `http://localhost:8080`

## Setup

1. **Install dependencies:**
   ```bash
   cd frontend
   npm install
   ```

2. **Configure environment (optional):**
   ```bash
   cp .env.example .env
   # Edit .env if your backend runs on a different URL
   ```

3. **Start development server:**
   ```bash
   npm run dev
   ```

   The app will open at `http://localhost:3000`

## Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `npm run lint` - Run ESLint

## Project Structure

```
src/
├── components/          # Reusable UI components
│   ├── common/         # Generic components (Button, Input, etc.)
│   ├── Layout/         # Layout components (Navbar)
│   ├── listings/       # Listing-specific components
│   └── modals/         # Modal components
├── context/            # React Context (Auth)
├── lib/                # Third-party lib configurations (Axios)
├── pages/              # Page components
│   ├── auth/          # Login, Register
│   └── listings/      # Browse, Detail, Create, Edit, MyListings
├── services/           # API service layer
├── types/              # TypeScript type definitions
├── App.tsx             # Main app component with routing
└── main.tsx            # App entry point
```

## Key Technologies

- **React 18** - UI library
- **TypeScript** - Type safety
- **Vite** - Build tool
- **React Router** - Client-side routing
- **Axios** - HTTP client
- **Tailwind CSS** - Styling
- **React Hot Toast** - Notifications
- **Lucide React** - Icons

## API Integration

The frontend connects to the Spring Boot backend via:
- **Base URL**: Configured in `.env` or defaults to `http://localhost:8080`
- **Authentication**: JWT tokens stored in localStorage
- **Interceptors**: Automatic token injection and error handling

### API Services

All API calls are organized in the `services/` directory:
- `authService.ts` - Authentication
- `listingService.ts` - Listings CRUD
- `wishlistService.ts` - Wishlist operations
- `reportService.ts` - Reporting
- `userService.ts` - User profile

## Security Features

✅ **No secrets in frontend** - Only public API URL configured  
✅ **JWT stored in localStorage** - Automatically sent with requests  
✅ **Protected routes** - Auth required for sensitive pages  
✅ **Automatic token refresh** - 401 redirects to login  
✅ **HTTPS ready** - Production deployment ready  

## Development Tips

1. **Mock data**: Backend must be running for full functionality
2. **Hot reload**: Vite provides instant HMR
3. **TypeScript**: All types are defined in `src/types/index.ts`
4. **Styling**: Use Tailwind utility classes
5. **Error handling**: All API errors show toast notifications

## Building for Production

```bash
npm run build
```

Output will be in `dist/` directory. Serve with any static file server.

### Environment Variables for Production

Set `VITE_API_URL` to your production API URL during build:

```bash
VITE_API_URL=https://api.campusswap.com npm run build
```

## Troubleshooting

**CORS errors?**
- Backend must allow requests from `http://localhost:3000`
- Check Spring Security CORS configuration

**401 Unauthorized?**
- Token might be expired
- Try logging out and back in

**Images not loading?**
- S3 URLs must be publicly accessible
- Check backend image upload configuration

## License

Campus Swap - Marketplace for College Students
